// Minimal dependency-free canvas charts used to visualize portfolio allocation and dashboard trend data.

const ASSET_TYPE_COLORS = {
  STOCK: '#2563eb',
  BOND: '#7c3aed',
  CASH: '#059669',
};

function drawPieChart(canvas, slices) {
  if (!canvas) return;
  const ctx = canvas.getContext('2d');
  const width = canvas.width;
  const height = canvas.height;
  ctx.clearRect(0, 0, width, height);

  const total = slices.reduce((sum, slice) => sum + slice.value, 0);
  const cx = width / 2;
  const cy = height / 2;
  const radius = Math.min(cx, cy) - 6;

  if (total <= 0) {
    ctx.beginPath();
    ctx.arc(cx, cy, radius, 0, Math.PI * 2);
    ctx.fillStyle = '#e2e8f0';
    ctx.fill();
    ctx.beginPath();
    ctx.arc(cx, cy, radius * 0.55, 0, Math.PI * 2);
    ctx.fillStyle = '#ffffff';
    ctx.fill();
    return;
  }

  let startAngle = -Math.PI / 2;
  slices.forEach((slice) => {
    const sliceAngle = (slice.value / total) * Math.PI * 2;
    ctx.beginPath();
    ctx.moveTo(cx, cy);
    ctx.arc(cx, cy, radius, startAngle, startAngle + sliceAngle);
    ctx.closePath();
    ctx.fillStyle = slice.color;
    ctx.fill();
    startAngle += sliceAngle;
  });

  // Punch a hole in the middle to render as a donut chart.
  ctx.beginPath();
  ctx.arc(cx, cy, radius * 0.55, 0, Math.PI * 2);
  ctx.fillStyle = '#ffffff';
  ctx.fill();
}

function drawAllocationChart(canvas, slices) {
  drawPieChart(canvas, slices);
}

function drawBarChart(canvas, bars) {
  if (!canvas) return;
  const ctx = canvas.getContext('2d');
  const width = canvas.width;
  const height = canvas.height;
  ctx.clearRect(0, 0, width, height);

  if (!bars || bars.length === 0) {
    ctx.fillStyle = '#e2e8f0';
    ctx.fillRect(0, 0, width, height);
    ctx.fillStyle = '#94a3b8';
    ctx.font = '13px sans-serif';
    ctx.textAlign = 'center';
    ctx.textBaseline = 'middle';
    ctx.fillText('No sector data available', width / 2, height / 2);
    return;
  }

  const padding = { top: 20, right: 20, bottom: 52, left: 64 };
  const chartWidth = width - padding.left - padding.right;
  const chartHeight = height - padding.top - padding.bottom;
  const maxValue = Math.max(...bars.map((b) => b.value), 0);
  const barCount = bars.length;
  const gap = 10;
  const barWidth = Math.max(14, (chartWidth - gap * (barCount + 1)) / barCount);

  // Axes
  ctx.strokeStyle = '#cbd5e1';
  ctx.lineWidth = 1;
  ctx.beginPath();
  ctx.moveTo(padding.left, padding.top);
  ctx.lineTo(padding.left, height - padding.bottom);
  ctx.lineTo(width - padding.right, height - padding.bottom);
  ctx.stroke();

  // Y-axis grid + labels
  const gridLines = 4;
  ctx.font = '11px sans-serif';
  ctx.textAlign = 'right';
  ctx.textBaseline = 'middle';
  for (let i = 0; i <= gridLines; i += 1) {
    const y = padding.top + (chartHeight / gridLines) * i;
    const value = maxValue - (maxValue / gridLines) * i;
    ctx.strokeStyle = '#e2e8f0';
    ctx.lineWidth = 0.5;
    ctx.beginPath();
    ctx.moveTo(padding.left, y);
    ctx.lineTo(width - padding.right, y);
    ctx.stroke();
    ctx.fillStyle = '#94a3b8';
    ctx.fillText(value.toLocaleString(undefined, { maximumFractionDigits: 0 }), padding.left - 8, y);
  }

  // Bars
  bars.forEach((bar, i) => {
    const x = padding.left + gap + i * (barWidth + gap);
    const barH = maxValue > 0 ? (bar.value / maxValue) * chartHeight : 0;
    const y = padding.top + chartHeight - barH;

    ctx.fillStyle = bar.color || '#2563eb';
    ctx.fillRect(x, y, barWidth, barH);

    // Value label on top of bar
    if (barH > 20) {
      ctx.fillStyle = '#fff';
      ctx.textAlign = 'center';
      ctx.textBaseline = 'top';
      ctx.font = 'bold 10px sans-serif';
      ctx.fillText(bar.value.toLocaleString(undefined, { maximumFractionDigits: 0 }), x + barWidth / 2, y + 5);
    }

    // X-axis label (rotated for readability)
    const label = bar.label.length > 14 ? bar.label.slice(0, 12) + '…' : bar.label;
    ctx.save();
    ctx.translate(x + barWidth / 2, height - padding.bottom + 8);
    ctx.rotate(-Math.PI / 5);
    ctx.fillStyle = '#64748b';
    ctx.font = '11px sans-serif';
    ctx.textAlign = 'right';
    ctx.textBaseline = 'top';
    ctx.fillText(label, 0, 0);
    ctx.restore();
  });
}

function drawLineChart(canvas, points) {
  if (!canvas) return;
  const ctx = canvas.getContext('2d');
  const width = canvas.width;
  const height = canvas.height;
  ctx.clearRect(0, 0, width, height);

  if (!points || points.length === 0) {
    ctx.fillStyle = '#e2e8f0';
    ctx.fillRect(0, 0, width, height);
    return;
  }

  const padding = { top: 18, right: 24, bottom: 48, left: 64 };
  const chartWidth = width - padding.left - padding.right;
  const chartHeight = height - padding.top - padding.bottom;
  const entries = points.map((point) => ({
    ts: new Date(point.label).getTime(),
    value: Math.max(0, Number(point.value) || 0),
    label: point.label,
  })).filter((entry) => Number.isFinite(entry.ts));
  if (entries.length === 0) return;

  const axisY = height - padding.bottom;
  const values = entries.map((entry) => entry.value);
  const min = 0;
  const rawMax = Math.max(...values, 0);
  const max = rawMax <= 0 ? 1 : Math.ceil(rawMax * 1.08);
  const range = max - min || 1;
  const minTs = Math.min(...entries.map((entry) => entry.ts));
  const maxTs = Math.max(...entries.map((entry) => entry.ts));
  const minHour = 3600000;
  const tsRange = Math.max(maxTs - minTs, minHour);
  const useDayLabels = tsRange >= 86400000;

  ctx.strokeStyle = '#cbd5e1';
  ctx.lineWidth = 1;
  ctx.beginPath();
  ctx.moveTo(padding.left, padding.top);
  ctx.lineTo(padding.left, axisY);
  ctx.lineTo(width - padding.right, axisY);
  ctx.stroke();

  const gridLines = 4;
  ctx.fillStyle = '#94a3b8';
  ctx.font = '11px sans-serif';
  ctx.textAlign = 'right';
  ctx.textBaseline = 'middle';

  for (let i = 0; i <= gridLines; i += 1) {
    const y = padding.top + (chartHeight / gridLines) * i;
    const value = Math.max(0, max - (range / gridLines) * i);
    ctx.strokeStyle = '#e2e8f0';
    ctx.beginPath();
    ctx.moveTo(padding.left, y);
    ctx.lineTo(width - padding.right, y);
    ctx.stroke();
    ctx.fillText(value.toLocaleString(undefined, { maximumFractionDigits: 2 }), padding.left - 8, y);
  }

  const coords = entries.map((entry) => {
    const x = padding.left + ((entry.ts - minTs) / tsRange) * chartWidth;
    const normalized = (entry.value - min) / range;
    const y = padding.top + chartHeight - normalized * chartHeight;
    return { x, y, point: entry };
  });

  const areaGradient = ctx.createLinearGradient(0, padding.top, 0, axisY);
  areaGradient.addColorStop(0, 'rgba(37, 99, 235, 0.22)');
  areaGradient.addColorStop(1, 'rgba(37, 99, 235, 0.02)');

  ctx.beginPath();
  coords.forEach((coord, index) => {
    if (index === 0) ctx.moveTo(coord.x, coord.y);
    else ctx.lineTo(coord.x, coord.y);
  });
  ctx.lineTo(coords[coords.length - 1].x, axisY);
  ctx.lineTo(coords[0].x, axisY);
  ctx.closePath();
  ctx.fillStyle = areaGradient;
  ctx.fill();

  ctx.strokeStyle = '#2563eb';
  ctx.lineWidth = 2.5;
  ctx.beginPath();
  coords.forEach((coord, index) => {
    if (index === 0) ctx.moveTo(coord.x, coord.y);
    else ctx.lineTo(coord.x, coord.y);
  });
  ctx.stroke();

  coords.forEach((coord) => {
    ctx.beginPath();
    ctx.arc(coord.x, coord.y, 3.5, 0, Math.PI * 2);
    ctx.fillStyle = '#2563eb';
    ctx.fill();
  });

  if (entries.length > 0) {
    ctx.textAlign = 'center';
    ctx.textBaseline = 'top';
    const firstTs = entries[0].ts;
    const dayInMs = 86400000; // milliseconds in a day
    coords.forEach((coord, index) => {
      ctx.fillStyle = '#64748b';
      // Calculate day number relative to first data point (1-based)
      const dayDiff = Math.floor((coord.point.ts - firstTs) / dayInMs) + 1;
      const label = `Day ${dayDiff}`;
      if (index === 0 || index === coords.length - 1 || index % Math.max(1, Math.ceil(coords.length / 6)) === 0) {
        ctx.fillText(label, coord.x, axisY + 12);
      }
    });
  }
}

