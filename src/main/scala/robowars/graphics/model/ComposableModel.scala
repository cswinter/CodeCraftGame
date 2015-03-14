package robowars.graphics.model


trait ComposableModel extends OldModel {
  def init(): DrawableModel
  def +(model: ComposableModel): ComposableModel
}
