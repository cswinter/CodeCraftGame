#version 400

in vec4 fragmentCol;

uniform float intensity;

out vec4 outputCol;

vec4 srgb(vec4 rgba) {
    vec3 rgb = vec3(rgba.x, rgba.y, rgba.z);
    vec3 mask = vec3(greaterThan(rgb, vec3(0.0031308)));
    vec3 result =
        mix(rgb * 12.92,
            pow(rgb, vec3(1.0 / 2.4)) * 1.055 - 0.055,
            mask);
    return vec4(result, rgba.w);
}

void main() {
    outputCol = srgb(intensity * fragmentCol);
}
