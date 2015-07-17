precision mediump float;

varying vec3 fragmentCol;

void main(void) {
    gl_FragColor = vec4(fragmentCol, 1);
}

