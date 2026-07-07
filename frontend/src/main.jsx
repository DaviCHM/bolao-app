import { createRoot } from 'react-dom/client';
import { StrictMode } from 'react';
import '@fontsource-variable/geist';
import '@fontsource-variable/newsreader';
import App from './App.jsx';
import './styles.css';

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <App />
  </StrictMode>,
);
