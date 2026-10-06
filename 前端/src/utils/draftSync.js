/**
 * 训练草稿同步决策。
 *
 * 背景：`training_session` 的草稿只在 `draft_version` 增加时才变化
 * （见 `TrainingMapper.saveDraft`：`SET draft=?, draft_version=draft_version+1 WHERE draft_version=?`），
 * 因此「版本相同 ⇒ 内容相同」。
 *
 * 曾经的缺陷：轮询用 `serverVersion >= localVersion` 判断「服务端更新」。
 * 提交回答后本地 draft/savedDraft 都被清空（判定为「未修改」），
 * 于是一个**提交之前发出的旧快照**（版本号恰好等于本地）会把上一个问题的回答写回输入框。
 *
 * @returns {'use'|'conflict'|'keep'}
 *  - use      采用服务端草稿
 *  - conflict 服务端版本更高但本地有未保存修改 → 交给用户处理
 *  - keep     保持本地（含「旧快照」情形）
 */
export function nextDraftAction({ force = false, hasSession = false, untouched = false, serverVersion = 0, localVersion = 0 } = {}) {
  if (force || !hasSession) return 'use'
  if (Number(serverVersion) > Number(localVersion)) return untouched ? 'use' : 'conflict'
  return 'keep'
}
