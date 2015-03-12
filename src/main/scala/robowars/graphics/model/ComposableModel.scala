package robowars.graphics.model


trait ComposableModel extends Model {
  def init(): DrawableModel
  def +(model: ComposableModel): ComposableModel
}
