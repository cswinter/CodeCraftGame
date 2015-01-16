#version 400

in vec2 vertexPos;
in vec2 texCoords;

out vec2 TexCoords;

void main()
{
    gl_Position = vec4(vertexPos.x, vertexPos.y, 0.0f, 1.0f);
    TexCoords = texCoords;
}
