package cwinter.codecraft.core.ai.destroyer

import cwinter.codecraft.core.ai.shared.AugmentedController


class DestroyerController(
  _name: Symbol,
  _context: DestroyerContext
) extends AugmentedController[DestroyerCommand, DestroyerContext](_name, _context)

