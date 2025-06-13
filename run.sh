#!/bin/bash

SESSION="jabber"
PORT=${1:-9000}  # 第1引数をPORTとして使用、デフォルトは9000

# 既存のセッションがあればkill
tmux kill-session -t $SESSION 2>/dev/null

# binディレクトリを作成
mkdir -p bin

# 新しいセッションを作成し、サーバーを左ペインで起動
tmux new-session -d -s $SESSION "javac -d bin src/*.java && java -cp bin JabberServer $PORT; read"

# 右側にペインを分割し、クライアント起動確認スクリプトを実行（PORTを環境変数として渡す）
tmux split-window -h -t $SESSION "PORT=$PORT bash scripts/client_prompt.sh"

# ペインのサイズを調整（任意）
tmux select-layout -t $SESSION even-horizontal

# tmuxセッションにアタッチ
tmux attach -t $SESSION