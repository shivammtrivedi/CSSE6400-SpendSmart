const http = require("http");
const app = require("./app");
const server = http.createServer(app);
const user = require('./models/user');
const { seedDemoAccount } = require('./config/seedData');

const { API_PORT } = process.env;
const port = process.env.PORT || API_PORT;
const routes = require('./routes/bankApiRoutes');
routes(app);

app.get('/health', (req, res) => res.status(200).json({ status: 'ok' }));

app.use(function(req, res) {
    res.status(404).send({ url: req.originalUrl + ' not found' });
    })
    // server listening 
server.listen(port, () => {
    console.log(`Server running on port ${port}`);
    seedDemoAccount().catch((err) => {
        console.error("Bank API seed failed", err);
    });
});
