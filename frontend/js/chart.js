// Minimal dependency-free canvas donut chart used to visualize portfolio allocation by asset type.

const ASSET_TYPE_COLORS = {
  STOCK: '#2563eb',
  BOND: '#7c3aed',
  CASH: '#059669',
};

function drawAllocationChart(canvas, slices) {
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
