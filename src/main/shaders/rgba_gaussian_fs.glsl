#version 400

in vec4 fragmentCol;

out vec4 outputCol;


void main() {
	float alpha = exp(-5 * fragmentCol.w * fragmentCol.w);
    outputCol = vec4(fragmentCol.x, fragmentCol.y, fragmentCol.z, alpha);
}
