# AI SSE应用构建和运行Makefile

# 定义变量
JAVA=java
MVN=mvn
JAR_PATH=target/sse-0.0.1-SNAPSHOT.jar
DB_NAME=agent

# 默认目标
.PHONY: all
all: run-backend

# 清理和构建项目
.PHONY: clean
clean:
	$(MVN) clean

# 编译项目（跳过测试）
.PHONY: build
build:
	$(MVN) clean package -DskipTests

# 创建数据库（如果不存在）
.PHONY: create-db
create-db:
	@echo "创建数据库 $(DB_NAME) (如果不存在)..."
	@mysql -u root -p88888888 -e "CREATE DATABASE IF NOT EXISTS $(DB_NAME);" || echo "创建数据库失败，请检查MySQL是否运行，以及密码是否正确"

# 运行后端服务
.PHONY: run-backend
run-backend: build create-db
	@echo "启动后端服务..."
	$(JAVA) -jar $(JAR_PATH)

# 运行前端服务
.PHONY: run-frontend
run-frontend:
	@echo "启动前端服务..."
	cd frontend && npm start

# 运行全栈应用（后端+前端）
.PHONY: run-all
run-all: build create-db
	@echo "启动后端服务（后台运行）..."
	$(JAVA) -jar $(JAR_PATH) &
	@echo "启动前端服务..."
	cd frontend && npm start

# 检查服务状态
.PHONY: check
check:
	@echo "检查后端服务状态..."
	@curl -s http://localhost:8080/api/conversations?userId=1 || echo "后端服务未运行或存在问题"
	@echo "\n检查前端服务状态..."
	@curl -s http://localhost:3000 > /dev/null && echo "前端服务运行中" || echo "前端服务未运行或存在问题"

# 帮助信息
.PHONY: help
help:
	@echo "可用的命令:"
	@echo "  make build        - 构建后端应用"
	@echo "  make create-db    - 创建MySQL数据库（如果不存在）"
	@echo "  make run-backend  - 构建并运行后端应用"
	@echo "  make run-frontend - 运行前端应用"
	@echo "  make run-all      - 运行整个应用（后端+前端）"
	@echo "  make check        - 检查服务状态"
	@echo "  make clean        - 清理构建文件"
	@echo "  make help         - 显示此帮助信息" 