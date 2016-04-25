package cwinter.codecraft.graphics.model

private[codecraft] class TheCompositeModelBuilderCache extends TypedCache {
  type V[a] = (Seq[ModelBuilder[_, Unit]], Seq[ModelBuilder[_, a]])
}

