#version 400

in vec4 fragmentCol;

out vec4 outputCol;

uniform float intensity;


void main() {
    float x = 1 - fragmentCol.w;
    float alpha = exp(-5 * x * x);
    outputCol = vec4(fragmentCol.x * intensity, fragmentCol.y * intensity, fragmentCol.z * intensity, alpha);
}
