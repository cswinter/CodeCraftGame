attribute vec3 aVertexPosition;

uniform mat4 modelview;
uniform mat4 projection;

void main(void) {
    gl_Position = projection * modelview * vec4(aVertexPosition, 1.0);
}

