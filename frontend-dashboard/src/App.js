import React, { useState, useEffect } from 'react';
import axios from 'axios';
import './App.css';
import { Doughnut } from 'react-chartjs-2';
import { Chart, ArcElement, Tooltip, Legend } from 'chart.js';
Chart.register(ArcElement, Tooltip, Legend);

function App() {
  const [strategies, setStrategies] = useState([]);
  const [search, setSearch] = useState("");

  useEffect(() => {
    axios.get('/results')
      .then(response => {
        setStrategies(response.data);
      })
      .catch(error => {
        console.error("There was an error fetching the data!", error);
      });
  }, []);

  // Filter strategies by search
  const filteredStrategies = strategies.filter(s =>
    s.strategyId && s.strategyId.toLowerCase().includes(search.toLowerCase())
  );

  // Helper for status badge
  const getStatusBadge = (status) => {
    let className = "status-badge ";
    let label = status;
    switch (status) {
      case "MATCHED":
        className += "matched";
        label = "Matched";
        break;
      case "PRICE_MISMATCH":
        className += "price-mismatch";
        label = "Price Mismatch";
        break;
      case "MISSING_LEG":
        className += "missing-leg";
        label = "Missing Leg";
        break;
      default:
        className += "other";
        break;
    }
    return <span className={className}>{label}</span>;
  };

  // Pie chart data
  const matched = strategies.filter(s => s.status === 'MATCHED').length;
  const priceMismatch = strategies.filter(s => s.status === 'PRICE_MISMATCH').length;
  const missingLeg = strategies.filter(s => s.status === 'MISSING_LEG').length;
  const breaks = priceMismatch + missingLeg;

  const chartData = {
    labels: ['Matched', 'Price Mismatch', 'Missing Leg'],
    datasets: [
      {
        data: [matched, priceMismatch, missingLeg],
        backgroundColor: [
          '#5eff7a',
          '#ff5e5e',
          '#ffb84d',
        ],
        borderColor: [
          '#232a36',
          '#232a36',
          '#232a36',
        ],
        borderWidth: 3,
        hoverOffset: 8,
      },
    ],
  };

  const chartOptions = {
    plugins: {
      legend: {
        display: true,
        position: 'bottom',
        labels: {
          color: '#b0b8c1',
          font: { size: 14, family: 'Montserrat, Segoe UI, Arial' },
          padding: 18,
        },
      },
      tooltip: {
        enabled: true,
        callbacks: {
          label: function(context) {
            const label = context.label || '';
            const value = context.raw || 0;
            return `${label}: ${value}`;
          }
        }
      }
    },
    cutout: '70%',
    responsive: true,
    maintainAspectRatio: false,
  };

  return (
    <div className="App">
      <nav className="sidebar-nav">
        <div className="nav-logo-placeholder"></div>
        <div className="nav-title">Synapse</div>
        <div className="nav-buttons">
          <button className="nav-btn" title="Dashboard">
            <span className="material-icons">dashboard</span>
          </button>
          <button className="nav-btn" title="Reports">
            <span className="material-icons">bar_chart</span>
          </button>
          <button className="nav-btn" title="Settings">
            <span className="material-icons">settings</span>
          </button>
          <button className="nav-btn" title="Help">
            <span className="material-icons">help_outline</span>
          </button>
        </div>
      </nav>
      <div className="main-content">
        <header className="App-header">
          <div className="logo-title-row">
            <span className="dashboard-title">Reconciliation Dashboard</span>
          </div>
        </header>
        <main>
          <div className="kpi-row">
            <div className="kpi-cards">
              <div className="kpi-card">
                <div className="kpi-label">Total Strategies Processed</div>
                <div className="kpi-value">{strategies.length}</div>
              </div>
              <div className="kpi-card">
                <div className="kpi-label">Total Matched</div>
                <div className="kpi-value">{matched}</div>
              </div>
              <div className="kpi-card">
                <div className="kpi-label">Total Breaks</div>
                <div className="kpi-value">{breaks}</div>
              </div>
            </div>
            <div className="kpi-chart-container">
              <Doughnut data={chartData} options={chartOptions} />
            </div>
          </div>
          {/* Search Bar - now left-aligned and wider */}
          <div className="search-bar-container search-bar-left">
            <input
              className="search-bar search-bar-wide"
              type="text"
              placeholder="Search by Strategy ID (e.g., IC_SPX)"
              value={search}
              onChange={e => setSearch(e.target.value)}
            />
          </div>
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
              {filteredStrategies.map(strategy => (
                <tr key={strategy.id} className={strategy.status}>
                  <td>{strategy.id}</td>
                  <td>{strategy.strategyId}</td>
                  <td className="status-cell">{getStatusBadge(strategy.status)}</td>
                  <td>{strategy.legCount}</td>
                  <td>{strategy.netPriceDifference.toFixed(4)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </main>
      </div>
    </div>
  );
}

export default App;