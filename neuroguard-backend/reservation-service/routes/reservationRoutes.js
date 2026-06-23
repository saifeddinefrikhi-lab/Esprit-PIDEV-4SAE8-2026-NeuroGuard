const express = require('express');
const router = express.Router();
const axios = require('axios');
const Reservation = require('../models/Reservation');

// Consultation service URL — via Eureka/gateway internally
const CONSULTATION_SERVICE_URL = process.env.CONSULTATION_SERVICE_URL || 'http://consultation-service:8090';

// ─── GET all reservations ───────────────────────────────────────────────────
router.get('/', async (req, res) => {
    try {
        const reservations = await Reservation.find().sort({ createdAt: -1 });
        res.json(reservations);
    } catch (err) {
        res.status(500).json({ message: err.message });
    }
});

// ─── GET reservations by patient ID ─────────────────────────────────────────
router.get('/patient/:patientId', async (req, res) => {
    try {
        const reservations = await Reservation.find({
            patientId: parseInt(req.params.patientId)
        }).sort({ createdAt: -1 });
        res.json(reservations);
    } catch (err) {
        res.status(500).json({ message: err.message });
    }
});

// ─── GET reservations by provider ID ────────────────────────────────────────
router.get('/provider/:providerId', async (req, res) => {
    try {
        const reservations = await Reservation.find({
            providerId: parseInt(req.params.providerId)
        }).sort({ createdAt: -1 });
        res.json(reservations);
    } catch (err) {
        res.status(500).json({ message: err.message });
    }
});

// ─── GET pending reservations for a provider ────────────────────────────────
router.get('/provider/:providerId/pending', async (req, res) => {
    try {
        const reservations = await Reservation.find({
            providerId: parseInt(req.params.providerId),
            status: 'PENDING'
        }).sort({ createdAt: -1 });
        res.json(reservations);
    } catch (err) {
        res.status(500).json({ message: err.message });
    }
});

// ─── GET a single reservation by ID ─────────────────────────────────────────
router.get('/:id', async (req, res) => {
    try {
        const reservation = await Reservation.findById(req.params.id);
        if (!reservation) {
            return res.status(404).json({ message: 'Reservation not found' });
        }
        res.json(reservation);
    } catch (err) {
        res.status(500).json({ message: err.message });
    }
});

// ─── POST create a reservation ───────────────────────────────────────────────
router.post('/', async (req, res) => {
    const { patientId, providerId, reservationDate, timeSlot, consultationType, notes, status } = req.body;

    if (!patientId || !providerId || !reservationDate || !timeSlot) {
        return res.status(400).json({
            message: 'Missing required fields: patientId, providerId, reservationDate, timeSlot'
        });
    }

    const reservation = new Reservation({
        patientId,
        providerId,
        reservationDate,
        timeSlot,
        consultationType: consultationType || 'ONLINE',
        notes: notes || '',
        status: status || 'PENDING'
    });

    try {
        const saved = await reservation.save();
        res.status(201).json(saved);
    } catch (err) {
        res.status(400).json({ message: err.message });
    }
});

// ─── PUT update a reservation ────────────────────────────────────────────────
router.put('/:id', async (req, res) => {
    try {
        const reservation = await Reservation.findById(req.params.id);
        if (!reservation) {
            return res.status(404).json({ message: 'Reservation not found' });
        }
        if (reservation.status !== 'PENDING') {
            return res.status(400).json({ message: 'Can only update PENDING reservations' });
        }
        const { reservationDate, timeSlot, consultationType, notes } = req.body;
        if (reservationDate) reservation.reservationDate = reservationDate;
        if (timeSlot) reservation.timeSlot = timeSlot;
        if (consultationType) reservation.consultationType = consultationType;
        if (notes !== undefined) reservation.notes = notes;

        const updated = await reservation.save();
        res.json(updated);
    } catch (err) {
        res.status(400).json({ message: err.message });
    }
});

// ─── POST accept a reservation — also triggers consultation creation ──────────
router.post('/:id/accept', async (req, res) => {
    try {
        const reservation = await Reservation.findById(req.params.id);
        if (!reservation) {
            return res.status(404).json({ message: 'Reservation not found' });
        }
        if (reservation.status !== 'PENDING') {
            return res.status(400).json({ message: 'Only PENDING reservations can be accepted' });
        }

        try {
            // Build ISO start time from reservationDate + timeSlot
            const dateStr = reservation.reservationDate.includes('T')
                ? reservation.reservationDate.split('T')[0]
                : reservation.reservationDate;
            const timeStr = reservation.timeSlot || '09:00';
            const startTime = `${dateStr}T${timeStr}:00`;

            // End time = start + 30 min
            const pad = (n) => n.toString().padStart(2, '0');
            const formatLocalDateTime = (d) =>
                `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
            const startDate = new Date(startTime);
            const endDate = new Date(startDate.getTime() + 30 * 60 * 1000);
            const endTime = formatLocalDateTime(endDate);

            const rawType = (reservation.consultationType || '').toString().toUpperCase();
            const consultationType =
                rawType === 'PRESENTIAL' || rawType === 'IN_PERSON'
                    ? 'PRESENTIAL'
                    : 'ONLINE';

            const consultationPayload = {
                title: `Consultation - ${dateStr} ${timeStr}`,
                description: reservation.notes || 'Scheduled via reservation',
                type: consultationType,
                startTime: startTime,
                endTime: endTime,
                patientId: reservation.patientId,
                providerId: reservation.providerId
            };

            console.log('[Accept] Creating consultation at', CONSULTATION_SERVICE_URL, ':', consultationPayload);

            const consultationRes = await axios.post(
                `${CONSULTATION_SERVICE_URL}/api/consultations/internal`,
                consultationPayload,
                { timeout: 10000 }
            );

            if (!consultationRes.data || !consultationRes.data.id) {
                throw new Error('Consultation created but no ID returned');
            }

            reservation.status = 'ACCEPTED';
            reservation.consultationId = consultationRes.data.id;
            const updated = await reservation.save();
            console.log('[Accept] Consultation created with ID:', consultationRes.data.id);
            return res.json(updated);
        } catch (consultationErr) {
            console.error('[Accept] Failed to create consultation:', consultationErr.message);
            return res.status(502).json({
                message: 'Failed to accept reservation because consultation creation failed',
                error: consultationErr.message
            });
        }
    } catch (err) {
        res.status(500).json({ message: err.message });
    }
});

// ─── POST reject a reservation ───────────────────────────────────────────────
router.post('/:id/reject', async (req, res) => {
    try {
        const reservation = await Reservation.findById(req.params.id);
        if (!reservation) {
            return res.status(404).json({ message: 'Reservation not found' });
        }
        if (reservation.status !== 'PENDING') {
            return res.status(400).json({ message: 'Only PENDING reservations can be rejected' });
        }
        reservation.status = 'REJECTED';
        const updated = await reservation.save();
        res.json(updated);
    } catch (err) {
        res.status(500).json({ message: err.message });
    }
});

// ─── DELETE a reservation ─────────────────────────────────────────────────────
router.delete('/:id', async (req, res) => {
    try {
        const reservation = await Reservation.findById(req.params.id);
        if (!reservation) {
            return res.status(404).json({ message: 'Reservation not found' });
        }
        reservation.status = 'DELETED';
        await reservation.save();
        res.json({ message: 'Reservation deleted' });
    } catch (err) {
        res.status(500).json({ message: err.message });
    }
});

module.exports = router;
