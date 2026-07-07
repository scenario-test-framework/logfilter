NAME    := logfilter
VERSION := $(shell git describe --tags --always --dirty 2>/dev/null || echo dev)
LDFLAGS := -s -w

.PHONY: all build test vet clean cross

all: vet test build

build:
	go build -trimpath -ldflags "$(LDFLAGS)" -o $(NAME) .

test:
	go test ./...

vet:
	go vet ./...

clean:
	rm -f $(NAME)
	rm -rf dist

# クロスコンパイル (配布用)
cross:
	mkdir -p dist
	GOOS=linux   GOARCH=amd64 go build -trimpath -ldflags "$(LDFLAGS)" -o dist/$(NAME)_$(VERSION)_linux_amd64/$(NAME) .
	GOOS=linux   GOARCH=arm64 go build -trimpath -ldflags "$(LDFLAGS)" -o dist/$(NAME)_$(VERSION)_linux_arm64/$(NAME) .
	GOOS=darwin  GOARCH=arm64 go build -trimpath -ldflags "$(LDFLAGS)" -o dist/$(NAME)_$(VERSION)_darwin_arm64/$(NAME) .
	GOOS=windows GOARCH=amd64 go build -trimpath -ldflags "$(LDFLAGS)" -o dist/$(NAME)_$(VERSION)_windows_amd64/$(NAME).exe .
	cd dist && for d in */; do tar czf "$${d%/}.tar.gz" "$$d"; done
	ls -l dist
