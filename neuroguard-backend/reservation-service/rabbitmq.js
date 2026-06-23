const amqp = require('amqplib');
const Reservation = require('./models/Reservation');

async function connectRabbitMQ() {
    try {
        const rabbitUrl = process.env.RABBITMQ_URL || 'amqp://guest:guest@localhost:5672';
        const connection = await amqp.connect(rabbitUrl);
        const channel = await connection.createChannel();
        
        const exchange = 'consultation.exchange';
        const queue = 'reservation.queue';
        const routingKey = 'consultation.cancelled';

        await channel.assertExchange(exchange, 'topic', { durable: true });
        await channel.assertQueue(queue, { durable: true });
        await channel.bindQueue(queue, exchange, routingKey);

        console.log(`Waiting for messages in ${queue}. To exit press CTRL+C`);

        channel.consume(queue, async (msg) => {
            if (msg !== null) {
                const messageContent = JSON.parse(msg.content.toString());
                console.log("Received async message:", messageContent);
                
                try {
                    // Si on reçoit un message d'annulation de consultation, on annule la réservation
                    const consultationId = messageContent.consultationId;
                    await Reservation.updateMany(
                        { consultationId: consultationId },
                        { status: 'CANCELLED' }
                    );
                    console.log(`Reservations for consultation ${consultationId} cancelled.`);
                } catch (err) {
                    console.error("Error processing message:", err);
                }
                
                channel.ack(msg);
            }
        });
    } catch (error) {
        console.error("RabbitMQ connection failed, retrying in 5 seconds...", error);
        setTimeout(connectRabbitMQ, 5000);
    }
}

module.exports = connectRabbitMQ;
