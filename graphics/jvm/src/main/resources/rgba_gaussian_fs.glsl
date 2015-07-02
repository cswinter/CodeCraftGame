#version 400

in vec4 fragmentCol;

out vec4 outputCol;


void main() {
    float x = 1 - fragmentCol.w;
    float alpha = exp(-5 * x * x);
    outputCol = vec4(fragmentCol.x, fragmentCol.y, fragmentCol.z, alpha);
}
