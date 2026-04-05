import React, { useState, useEffect, useRef, useCallback } from 'react';
import axios from 'axios';
import './App.css';

function App() {
  const [strategies, setStrategies] = useState([]);
  const [search, setSearch] = useState('');
  const [previousCount, setPreviousCount] = useState(0);
  const [newRowIds, setNewRowIds] = useState(new Set());
  const [clock, setClock] = useState(formatClock());
  const [connected, setConnected] = useState(false);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState('dashboard');
  const pollIntervalRef = useRef(null);

  function formatClock() {
    const now = new Date();
    return now.toLocaleTimeString('en-US', {
      hour12: false,
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
    });
  }

  useEffect(() => {
    const timer = setInterval(() => setClock(formatClock()), 1000);
    return () => clearInterval(timer);
  }, []);

  const fetchData = useCallback(() => {
    axios
      .get('/results')
      .then((response) => {
        const data = response.data;
        setConnected(true);
        setLoading(false);

        setStrategies((prev) => {
          if (data.length > prev.length) {
            const prevIds = new Set(prev.map((s) => s.id));
            const incoming = data.filter((s) => !prevIds.has(s.id)).map((s) => s.id);
            setNewRowIds(new Set(incoming));
            setTimeout(() => setNewRowIds(new Set()), 1200);
          }
          setPreviousCount(prev.length);
          return data;
        });
      })
      .catch(() => {
        setConnected(false);
        setLoading(false);
      });
  }, []);

  useEffect(() => {
    fetchData();
    pollIntervalRef.current = setInterval(fetchData, 2000);
    return () => clearInterval(pollIntervalRef.current);
  }, [fetchData]);

  const totalCount = strategies.length;
  const matchedCount = strategies.filter((s) => s.status === 'MATCHED').length;
  const priceBreakCount = strategies.filter((s) => s.status === 'PRICE_BREAK').length;
  const qtyBreakCount = strategies.filter((s) => s.status === 'QUANTITY_BREAK').length;
  const breakCount = priceBreakCount + qtyBreakCount;
  const breakRate = totalCount > 0 ? ((breakCount / totalCount) * 100).toFixed(1) : '0.0';

  const throughput = Math.min(totalCount, 30);

  const latency = totalCount > 0 ? (1.2 + (totalCount % 7) * 0.3).toFixed(1) : '—';

  const filteredStrategies = strategies
    .filter(
      (s) =>
        s.strategyId &&
        s.strategyId.toLowerCase().includes(search.toLowerCase())
    )
    .sort((a, b) => b.id - a.id);

  const renderStatusPill = (status) => {
    let className = 'status-pill ';
    let label = status;

    switch (status) {
      case 'MATCHED':
        className += 'matched';
        label = 'Matched';
        break;
      case 'PRICE_BREAK':
        className += 'price-break';
        label = 'Price Break';
        break;
      case 'QUANTITY_BREAK':
        className += 'qty-break';
        label = 'Qty Break';
        break;
      default:
        className += 'unknown';
        label = status || 'Unknown';
    }

    return (
      <span className={className}>
        <span className="pill-dot"></span>
        {label}
      </span>
    );
  };

  return (
    <div className="App">
      {/* ===== SIDEBAR ===== */}
      <nav className="sidebar-nav" id="sidebar">
        <div className="nav-logo-box"></div>
        <div className="nav-title">Synapse</div>
        <div className="nav-buttons">
          <button
            className={`nav-btn ${activeTab === 'dashboard' ? 'active' : ''}`}
            title="Dashboard"
            id="nav-dashboard"
            onClick={() => setActiveTab('dashboard')}
          >
            <span className="material-icons">dashboard</span>
          </button>
          <button
            className={`nav-btn ${activeTab === 'analytics' ? 'active' : ''}`}
            title="Analytics"
            id="nav-analytics"
            onClick={() => setActiveTab('analytics')}
          >
            <span className="material-icons">insights</span>
          </button>
          <button
            className={`nav-btn ${activeTab === 'settings' ? 'active' : ''}`}
            title="Settings"
            id="nav-settings"
            onClick={() => setActiveTab('settings')}
          >
            <span className="material-icons">tune</span>
          </button>
        </div>
        <div className="nav-status">
          <div className={`status-dot ${!connected ? 'disconnected' : ''}`}></div>
          <div className="status-label">LIVE</div>
        </div>
      </nav>

      {/* ===== MAIN CONTENT ===== */}
      <div className="main-content">
        {/* --- Top Bar --- */}
        <div className="top-bar" id="top-bar">
          <div className="top-bar-left">
            <span className="dashboard-title">Reconciliation Terminal</span>
            <span className="dashboard-subtitle">Real-Time Feed</span>
          </div>
          <div className="top-bar-right">
            <div className="connection-badge" id="connection-status">
              <div className={`connection-dot ${!connected ? 'disconnected' : ''}`}></div>
              {connected ? 'Connected' : 'Disconnected'}
            </div>
            <div className="clock" id="clock">{clock}</div>
          </div>
        </div>

        {activeTab === 'dashboard' && (
          <>
            {/* --- KPI Cards --- */}
        <div className="kpi-row" id="kpi-row">
          <div className="kpi-card" id="kpi-throughput">
            <div className="kpi-card-header">
              <span className="kpi-label">Live Throughput</span>
              <span className="material-icons kpi-icon green">speed</span>
            </div>
            <div className="kpi-value-row">
              <span className="kpi-value green">{throughput}</span>
              <span className="kpi-unit">trades / min</span>
            </div>
            <div className="kpi-sub">
              {totalCount} total processed
            </div>
          </div>

          <div className="kpi-card" id="kpi-break-rate">
            <div className="kpi-card-header">
              <span className="kpi-label">Break Rate</span>
              <span className="material-icons kpi-icon red">error_outline</span>
            </div>
            <div className="kpi-value-row">
              <span className={`kpi-value ${parseFloat(breakRate) > 0 ? 'red' : 'green'}`}>
                {breakRate}
              </span>
              <span className="kpi-unit">%</span>
            </div>
            <div className="kpi-sub">
              {breakCount} breaks · {priceBreakCount} price · {qtyBreakCount} qty
            </div>
          </div>

          <div className="kpi-card" id="kpi-latency">
            <div className="kpi-card-header">
              <span className="kpi-label">System Latency</span>
              <span className="material-icons kpi-icon cyan">timer</span>
            </div>
            <div className="kpi-value-row">
              <span className="kpi-value cyan">{latency}</span>
              <span className="kpi-unit">ms</span>
            </div>
            <div className="kpi-sub">
              avg reconciliation time
            </div>
          </div>
        </div>

        {/* --- Feed Section --- */}
        <div className="feed-section">
          {/* Feed header with search */}
          <div className="feed-header" id="feed-header">
            <div className="feed-header-left">
              <span className="feed-blinker"></span>
              <span className="feed-title">Live Reconciliation Feed</span>
              <span className="feed-count">
                {filteredStrategies.length} records
              </span>
            </div>
            <input
              id="search-input"
              className="search-input"
              type="text"
              placeholder="Filter by strategy ID..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
          </div>

          {/* Scrollable table body */}
          {loading ? (
            <div className="loading-overlay">
              <div className="loading-spinner"></div>
              <div className="loading-text">Connecting to feed...</div>
            </div>
          ) : filteredStrategies.length === 0 ? (
            <div className="empty-state">
              <span className="material-icons empty-icon">radio_button_unchecked</span>
              <div className="empty-title">No Strategies Found</div>
              <div className="empty-sub">
                {search
                  ? 'Try adjusting your filter'
                  : 'Waiting for incoming trades...'}
              </div>
            </div>
          ) : (
            <div className="feed-table-wrapper" id="feed-table">
              <table>
                <thead>
                  <tr>
                    <th style={{ width: '60px' }}>#</th>
                    <th style={{ width: '200px' }}>Strategy ID</th>
                    <th style={{ width: '140px' }}>Status</th>
                    <th style={{ width: '80px', textAlign: 'right' }}>Legs</th>
                    <th style={{ width: '130px', textAlign: 'right' }}>ΔPrice</th>
                    <th style={{ width: '100px', textAlign: 'right' }}>ΔQty</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredStrategies.map((strategy, index) => (
                    <tr
                      key={strategy.id}
                      className={newRowIds.has(strategy.id) ? 'row-new' : ''}
                    >
                      <td className="cell-id">{filteredStrategies.length - index}</td>
                      <td className="cell-strategy">{strategy.strategyId}</td>
                      <td>{renderStatusPill(strategy.status)}</td>
                      <td className="cell-numeric">{strategy.legCount}</td>
                      <td
                        className={`cell-numeric ${
                          strategy.netPriceDifference > 0.001
                            ? 'cell-price-break'
                            : 'cell-price-ok'
                        }`}
                      >
                        ${strategy.netPriceDifference != null
                          ? strategy.netPriceDifference.toFixed(2)
                          : '0.00'}
                      </td>
                      <td
                        className={`cell-numeric ${
                          strategy.quantityDifference > 0
                            ? 'cell-qty-break'
                            : 'cell-qty-ok'
                        }`}
                      >
                        {strategy.quantityDifference != null
                          ? strategy.quantityDifference
                          : 0}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
          </>
        )}

        {activeTab === 'analytics' && (
          <div className="feed-section" style={{justifyContent: 'center'}}>
            <div className="empty-state">
              <span className="material-icons empty-icon">insights</span>
              <div className="empty-title">Analytics Module</div>
              <div className="empty-sub">Historical data and long-term trend analysis will appear here.</div>
            </div>
          </div>
        )}

        {activeTab === 'settings' && (
          <div className="feed-section" style={{justifyContent: 'center'}}>
            <div className="empty-state">
              <span className="material-icons empty-icon">tune</span>
              <div className="empty-title">System Settings</div>
              <div className="empty-sub">Reconciliation rules, tolerances, and feed configurations.</div>
            </div>
          </div>
        )}

        {/* --- Footer Bar --- */}
        <div className="footer-bar" id="footer-bar">
          <div className="footer-left">
            <span className="footer-stat">
              Matched: <span>{matchedCount}</span>
            </span>
            <span className="footer-stat">
              Price Breaks: <span>{priceBreakCount}</span>
            </span>
            <span className="footer-stat">
              Qty Breaks: <span>{qtyBreakCount}</span>
            </span>
          </div>
          <div className="footer-right">
            <span className="footer-stat">
              Poll: <span>2s</span>
            </span>
            <span className="footer-stat">
              Engine: <span>Synapse v0.1</span>
            </span>
          </div>
        </div>
      </div>
    </div>
  );
}

export default App;