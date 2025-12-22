const { createProxyMiddleware } = require('http-proxy-middleware');

module.exports = function(app) {
  app.use(
    '/users',
    createProxyMiddleware({
      target: 'http://localhost:8082',
      changeOrigin: true,
    })
  );
  app.use(
    '/api/wallets',
    createProxyMiddleware({
      target: 'http://localhost:8083',
      changeOrigin: true,
    })
  );
  app.use(
    '/transactions',
    createProxyMiddleware({
      target: 'http://localhost:8081',
      changeOrigin: true,
    })
  );
};
