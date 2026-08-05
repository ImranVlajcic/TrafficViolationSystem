// Development config. `/api` is handled by proxy.conf.json, which forwards
// to http://localhost:8080 — see ng serve --proxy-config proxy.conf.json
// (already wired as the default serve config in angular.json).
export const environment = {
  production: false,
  apiUrl: '/api'
};