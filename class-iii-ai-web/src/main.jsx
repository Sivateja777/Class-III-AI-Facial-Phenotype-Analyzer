import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './App.jsx'
import './index.css'

window.onerror = function(message, source, lineno, colno, error) {
  document.body.innerHTML = `<div style="padding: 20px; color: red; background: white; z-index: 9999; position: absolute; top: 0; left: 0; width: 100vw; height: 100vh;">
    <h2>App Crashed!</h2>
    <p><strong>Message:</strong> ${message}</p>
    <p><strong>Source:</strong> ${source}:${lineno}:${colno}</p>
    <pre>${error?.stack}</pre>
  </div>`;
};

try {
  ReactDOM.createRoot(document.getElementById('root')).render(
    <React.StrictMode>
      <App />
    </React.StrictMode>,
  )
} catch (err) {
  document.body.innerHTML = `<div style="padding: 20px; color: red; background: white;"><h2>Top Level Error:</h2><pre>${err.stack}</pre></div>`;
}
