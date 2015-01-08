#version 400

in vec3 vp;

uniform mat4 projection;
uniform mat4 modelview;


void main () {
  gl_Position = projection * modelview * vec4(vp, 1.0);
}
