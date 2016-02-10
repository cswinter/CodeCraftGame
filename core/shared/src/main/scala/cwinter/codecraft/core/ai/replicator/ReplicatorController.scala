package cwinter.codecraft.core.ai.replicator

import cwinter.codecraft.core.ai.replicator.combat.ReplicatorCommand
import cwinter.codecraft.core.ai.shared.AugmentedController


class ReplicatorController(
  _name: Symbol,
  _context: ReplicatorContext
) extends AugmentedController[ReplicatorCommand, ReplicatorContext](_name, _context)
