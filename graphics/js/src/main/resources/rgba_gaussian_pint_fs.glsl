precision mediump float;

varying vec4 fragmentCol;

uniform float intensity;

void main() {
    float x = 1.0 - fragmentCol.w;
    float alpha = exp(-5.0 * x * x);
    gl_FragColor = vec4(fragmentCol.x * intensity, fragmentCol.y * intensity, fragmentCol.z * intensity, alpha);
}
