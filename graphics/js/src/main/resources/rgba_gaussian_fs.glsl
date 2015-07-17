precision mediump float;

varying vec4 fragmentCol;
void main(void) {
    float x = 1.0 - fragmentCol.w;
    float alpha = exp(-5.0 * x * x);
    gl_FragColor = vec4(fragmentCol.x, fragmentCol.y, fragmentCol.z, alpha);
}
