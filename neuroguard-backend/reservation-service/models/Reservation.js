const mongoose = require('mongoose');

const reservationSchema = new mongoose.Schema({
    patientId: {
        type: Number,
        required: true
    },
    providerId: {
        type: Number,
        required: true
    },
    reservationDate: {
        type: String,  // ISO string e.g. "2026-06-25T09:00:00"
        required: true
    },
    timeSlot: {
        type: String,  // "HH:mm"
        required: true
    },
    consultationType: {
        type: String,
        enum: ['ONLINE', 'PRESENTIAL'],
        default: 'ONLINE'
    },
    status: {
        type: String,
        enum: ['PENDING', 'ACCEPTED', 'REJECTED', 'DELETED', 'COMPLETED'],
        default: 'PENDING'
    },
    notes: {
        type: String,
        default: ''
    },
    consultationId: {
        type: Number,
        default: null
    },
    patientName: {
        type: String,
        default: ''
    },
    providerName: {
        type: String,
        default: ''
    }
}, {
    timestamps: true  // adds createdAt and updatedAt automatically
});

module.exports = mongoose.model('Reservation', reservationSchema);
