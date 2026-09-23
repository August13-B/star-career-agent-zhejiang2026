// 同名岗位使用独立 ID；坐标稳定，节点外观沿用用户提供的 ECharts HTML。
const palette = {
  target: { light: '#60A5FA', dark: '#1D4ED8' },
  promotion: { light: '#34D399', dark: '#059669' },
  transfer: { light: '#A78BFA', dark: '#5B21B6' }
}
const ceramicStyle = ({ light, dark }) => ({
  color: {
    type: 'radial', x: 0.25, y: 0.25, r: 0.55,
    colorStops: [
      { offset: 0, color: '#FFFFFF' },
      { offset: 0.2, color: light },
      { offset: 1, color: dark }
    ]
  },
  shadowColor: 'rgba(15, 23, 42, 0.15)',
  shadowBlur: 14,
  shadowOffsetX: 4,
  shadowOffsetY: 6,
  borderColor: 'rgba(255, 255, 255, 0.85)',
  borderWidth: 1.5
})
const shorten = (value, length) => {
  const chars = Array.from(String(value || ''))
  return chars.length > length ? chars.slice(0, length - 1).join('') + '…' : chars.join('')
}
export function buildCareerGraph(graph) {
  const branches = Array.isArray(graph.branches) ? graph.branches : []
  const height = Math.max(380, branches.length * 132 + 48)
  const nodes = [{
    id: 'center', name: graph.center, x: 135, y: height / 2,
    symbol: 'circle', symbolSize: 90,
    itemStyle: ceramicStyle(palette.target),
    label: { formatter: shorten(graph.center, 20) }
  }]
  const links = []
  const order = { target: 0, promotion: 1, transfer: 2 }
  const sorted = branches.map((branch, index) => ({ branch, index }))
    .sort((a, b) => (order[a.branch.kind] ?? 0) - (order[b.branch.kind] ?? 0))
  sorted.forEach(({ branch: b, index: i }, row) => {
    const color = palette[b.kind] || palette.target
    const y = height / 2 + (row - (branches.length - 1) / 2) * 132
    const id = `branch-${i}`
    nodes.push({
      id, name: b.name, branchIndex: i, x: 460, y,
      symbol: 'circle', symbolSize: 58,
      itemStyle: ceramicStyle(color),
      label: { formatter: shorten(b.name, 22) }
    })
    links.push({ source: 'center', target: id, lineStyle: { color: color.dark, opacity: .45 } })
    const skills = (Array.isArray(b.skills) ? b.skills : []).slice(0, 3)
    skills.forEach((skill, j) => {
      const skillId = `skill-${i}-${j}`
      nodes.push({
        id: skillId, name: skill, branchIndex: i, x: 800, y: y + (j - (skills.length - 1) / 2) * 38,
        symbol: 'circle', symbolSize: 38,
        itemStyle: ceramicStyle(color),
        label: { formatter: shorten(skill, 18), fontSize: 11 }
      })
      links.push({ source: id, target: skillId, lineStyle: { color: color.dark, opacity: .45 } })
    })
  })
  return { nodes, links, width: 1100, height }
}
