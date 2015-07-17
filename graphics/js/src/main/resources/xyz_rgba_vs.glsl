attribute vec3 vertexPos;
attribute vec4 vertexCol;

varying vec4 fragmentCol;

uniform mat4 modelview;
uniform mat4 projection;

void main (void) {
    gl_Position = projection * modelview * vec4(vertexPos, 1.0);

    fragmentCol = vertexCol;
}
