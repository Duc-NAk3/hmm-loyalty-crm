/** Logo hoa mai 5 cánh + tên chuỗi. */
export function BrandMark({ size = 28 }: { size?: number }) {
  return (
    <svg width={size} height={size} viewBox="0 0 32 32" aria-hidden>
      <g transform="translate(16 16)" fill="#F5B301">
        {[0, 72, 144, 216, 288].map((r) => (
          <ellipse key={r} rx="5" ry="9" transform={`rotate(${r}) translate(0 -7)`} />
        ))}
      </g>
      <circle cx="16" cy="16" r="4" fill="#B45309" />
    </svg>
  )
}

export function Brand({ subtitle, light }: { subtitle?: string; light?: boolean }) {
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
      <BrandMark />
      <div style={{ lineHeight: 1.15 }}>
        <div style={{ fontWeight: 700, fontSize: 16, color: light ? '#fff' : undefined }}>Hoa Mai Mart</div>
        {subtitle && <div style={{ fontSize: 12, opacity: 0.75, color: light ? '#fff' : undefined }}>{subtitle}</div>}
      </div>
    </div>
  )
}
