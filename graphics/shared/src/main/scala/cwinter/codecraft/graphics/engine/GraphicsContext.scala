package cwinter.codecraft.graphics.engine

import cwinter.codecraft.graphics.model.{VBO, TheCompositeModelBuilderCache, TheModelCache}

private[codecraft] class GraphicsContext(
  val materials: RenderStack,
  val useTransposedModelview: Boolean,
  private[graphics] val modelCache: TheModelCache,
  private[graphics] val modelBuilderCache: TheCompositeModelBuilderCache
) {
  private[this] var vbos = List.empty[VBO]

  private[graphics] def createdTempVBO(vbo: VBO): Unit = vbos ::= vbo

  private[graphics] def freeTempVBOs(gl: Any): Unit = {
    vbos.foreach(_.dispose(gl))
    vbos = List.empty[VBO]
  }

  private[graphics] def dispose(gl: Any): Unit = {
    freeTempVBOs(gl)
    materials.dispose()
    modelCache.clear()
    modelBuilderCache.clear()
  }
}

