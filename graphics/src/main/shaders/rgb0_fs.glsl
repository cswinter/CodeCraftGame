#version 400

in vec3 fragmentCol;

out vec4 outputCol;


void main() {
    outputCol = vec4(fragmentCol, 0);
}