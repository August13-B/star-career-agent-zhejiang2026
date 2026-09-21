// 同名岗位使用独立 ID；固定分层坐标避免力导向布局遮挡文字。
const palette = {
  target: { fill: '#D9E2F4', stroke: '#4773CA', label: '目标方向' },
  promotion: { fill: '#E2F1D8', stroke: '#81975E', label: '进阶路径' },
  transfer: { fill: '#F9DADE', stroke: '#B76679', label: '迁移方向' }
}
const shorten = (value, length) => {
  const chars = Array.from(String(value || ''))
  return chars.length > length ? chars.slice(0, length - 1).join('') + '…' : chars.join('')
}
export function buildCareerGraph(graph) {
  const branches = Array.isArray(graph.branches) ? graph.branches : []
  const height = Math.max(380, branches.length * 132 + 48)
  const nodes = [{
    id: 'center', name: graph.center, x: 130, y: height / 2,
    symbol: 'rect', symbolSize: [176, 78],
    itemStyle: { color: '#FEF1C9', borderColor: '#AD9659', borderWidth: 1.3 },
    label: { formatter: '测评基准\n' + shorten(graph.center, 20), width: 152, fontWeight: 600 }
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
      id, name: b.name, branchIndex: i, x: 405, y,
      symbol: 'rect', symbolSize: [210, 70],
      itemStyle: { color: color.fill, borderColor: color.stroke, borderWidth: 1.3 },
      label: { formatter: color.label + '\n' + shorten(b.name, 26), width: 188, fontWeight: 600 }
    })
    links.push({ source: 'center', target: id, lineStyle: { color: color.stroke, opacity: .5 } })
    const skills = (Array.isArray(b.skills) ? b.skills : []).slice(0, 3)
    skills.forEach((skill, j) => {
      const skillId = `skill-${i}-${j}`
      nodes.push({
        id: skillId, name: skill, branchIndex: i, x: 700, y: y + (j - (skills.length - 1) / 2) * 38,
        symbol: 'rect', symbolSize: [190, 32],
        itemStyle: { color: '#fff', borderColor: color.stroke, borderWidth: 1 },
        label: { formatter: shorten(skill, 15), width: 174, fontSize: 11 }
      })
      links.push({ source: id, target: skillId, lineStyle: { color: color.stroke, opacity: .6 } })
    })
  })
  return { nodes, links, width: 840, height }
}
