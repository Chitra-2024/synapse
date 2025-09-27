import React, { useState, useEffect } from 'react';
import axios from 'axios';
import './App.css';

function App() {
  const [strategies, setStrategies] = useState([]);

  useEffect(() => {
    // This function will be called once when the component loads
    axios.get('/results')
      .then(response => {
        setStrategies(response.data);
      })
      .catch(error => {
        console.error("There was an error fetching the data!", error);
      });
  }, []); // The empty array means this effect runs only once

  return (
    <div className="App">
      <header className="App-header">
        <h1>Project Synapse - Reconciliation Dashboard</h1>
      </header>
      <main>
        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>Strategy ID</th>
              <th>Status</th>
              <th>Leg Count</th>
              <th>Net Price Difference</th>
            </tr>
          </thead>
          <tbody>
            {strategies.map(strategy => (
              <tr key={strategy.id} className={strategy.status}>
                <td>{strategy.id}</td>
                <td>{strategy.strategyId}</td>
                <td className="status-cell">{strategy.status}</td>
                <td>{strategy.legCount}</td>
                <td>{strategy.netPriceDifference.toFixed(4)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </main>
    </div>
  );
}

export default App;