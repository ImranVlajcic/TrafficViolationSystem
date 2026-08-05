// Production config. There is no dev proxy in a production build, so this
// needs a real, reachable backend origin.
//
// TODO: replace with the actual deployed backend URL before shipping.
export const environment = {
  production: true,
  apiUrl: 'https://api.traficc.example.com/api'
};