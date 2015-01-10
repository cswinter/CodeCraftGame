#version 400

in vec2 vertexPos;
in vec3 vertexCol;

out vec3 fragmentCol;

uniform mat4 projection;
uniform mat4 modelview;


void main () {
  gl_Position = projection * modelview * vec4(vertexPos, 1.0, 1.0);

  fragmentCol = vertexCol;
}
