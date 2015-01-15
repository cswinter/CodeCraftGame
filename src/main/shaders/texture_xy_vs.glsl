#version 400

layout (location = 0) in vec2 vertexPos;
layout (location = 1) in vec2 texCoords;

out vec2 TexCoords;

void main()
{
    gl_Position = vec4(vertexPos.x, vertexPos.y, 0.0f, 1.0f);
    TexCoords = texCoords;
}
