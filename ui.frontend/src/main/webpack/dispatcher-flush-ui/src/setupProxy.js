const { createProxyMiddleware } = require('http-proxy-middleware');

const AEM_SDK_BASIC_AUTH = 'admin:admin';

module.exports = function(app) {
  app.use(
    '/etc',
    createProxyMiddleware({
      target: 'http://localhost:4502',
      changeOrigin: true,
      auth: AEM_SDK_BASIC_AUTH
    })
  );
};