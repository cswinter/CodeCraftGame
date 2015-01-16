#version 400

in vec2 TexCoords;
out vec4 color;

uniform sampler2D screenTexture;
uniform int orientation; // 0 for horizontal and 1 for vertical convolution
uniform vec2 texelSize;


float Gaussian(float x, float deviation) {
    return (1.0 / sqrt(2.0 * 3.141592 * deviation)) * exp(-((x * x) / (2.0 * deviation)));
}


void main()
{
    float deviation = 200;
    float strength = 1;
    vec4 col = vec4(0, 0, 0, 0);
    int width = 25;

    if (orientation == 0) {
        for (int x = -width; x < width + 1; x++) {
            vec4 texcol = texture(screenTexture, TexCoords + vec2(texelSize.x * x, 0));
            if (texcol.w > 0)
                col += strength * Gaussian(x, deviation) * texcol;
        }
    } else if (orientation == 1) {
        for (int y = -width; y < width + 1; y++) {
            vec4 texcol = texture(screenTexture, TexCoords + vec2(0, texelSize.y * y));
            col += strength * Gaussian(y, deviation) * texcol;
        }
    }

    col.w = 0;// texture(screenTexture, TexCoords).w;
    color = col;
}
