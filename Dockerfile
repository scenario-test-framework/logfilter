# syntax=docker/dockerfile:1

# --------------------------------------------------
# build stage
# --------------------------------------------------
FROM golang:1.26-alpine AS builder

WORKDIR /src

# 依存解決をレイヤキャッシュ
COPY go.mod go.sum ./
RUN go mod download

COPY . .

# timetzdata: タイムゾーンDBをバイナリに埋め込む (scratchで TZ 指定を有効にするため)
RUN CGO_ENABLED=0 go build -trimpath -tags timetzdata \
    -ldflags "-s -w" \
    -o /out/logfilter .

# --------------------------------------------------
# runtime stage
# --------------------------------------------------
FROM scratch

COPY --from=builder /out/logfilter /logfilter

# ログファイルのマウントポイント
WORKDIR /work

# nobody
USER 65534:65534

ENTRYPOINT ["/logfilter"]
CMD ["--help"]
