package cwinter.codecraft.core.ai.destroyer

import cwinter.codecraft.core.ai.shared.AugmentedController


private[codecraft] class DestroyerController(
  _context: DestroyerContext
) extends AugmentedController[DestroyerCommand, DestroyerContext](_context)

