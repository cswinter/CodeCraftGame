#version 400

in vec4 fragmentCol;

out vec4 outputCol;

uniform float intensity;


void main() {
	float alpha = exp(-5 * fragmentCol.w * fragmentCol.w);
    outputCol = vec4(fragmentCol.x * intensity, fragmentCol.y * intensity, fragmentCol.z * intensity, alpha);
}
