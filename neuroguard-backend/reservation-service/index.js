const express = require('express');
const mongoose = require('mongoose');
const cors = require('cors');
require('dotenv').config();
const eurekaHelper = require('./eureka-helper');
const connectRabbitMQ = require('./rabbitmq');

const app = express();
const PORT = process.env.PORT || 3000;

app.use(cors({
  // CORS is handled by the API Gateway (Spring Cloud Gateway).
  // This service sits behind the gateway and is not directly accessed by browsers.
  // We disable wildcard CORS here to prevent duplicate Access-Control-Allow-Origin headers.
  origin: false
}));
app.use(express.json());

// MongoDB connection
mongoose.connect(process.env.MONGODB_URI || 'mongodb://localhost:27017/reservation_db', {
    useNewUrlParser: true,
    useUnifiedTopology: true
})
.then(() => console.log('Connected to MongoDB'))
.catch(err => console.error('Could not connect to MongoDB', err));

// Routes
const reservationRoutes = require('./routes/reservationRoutes');
app.use('/api/reservations', reservationRoutes);

app.get('/info', (req, res) => {
    res.send({ status: 'UP' });
});

app.listen(PORT, () => {
    console.log(`Reservation Service listening on port ${PORT}`);
    // Register with Eureka
    eurekaHelper.registerWithEureka('reservation-service', PORT);
    // Connect to RabbitMQ for async communication
    connectRabbitMQ();
});
