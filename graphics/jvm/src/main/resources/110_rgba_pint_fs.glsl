#version 110

varying vec4 fragmentCol;

uniform float intensity;

void main() {
    gl_FragColor = intensity * fragmentCol;
}

