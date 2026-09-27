#version 150
uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
in vec2 texCoord;
in vec4 vertexColor;
out vec4 fragColor;
void main() {
    float coverage = texture(Sampler0, texCoord).a;
    fragColor = vec4(vertexColor.rgb, vertexColor.a * coverage) * ColorModulator;
}
