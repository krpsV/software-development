#!/bin/bash

SESSION="jabber_multi_test"
PORT=${1:-9000}
CLIENT_COUNT=${2:-3}

echo "複数クライアントテストを開始します..."
echo "ポート: $PORT"
echo "クライアント数: $CLIENT_COUNT"

# 既存のセッションがあればkill
tmux kill-session -t $SESSION 2>/dev/null

# binディレクトリを作成
mkdir -p bin

# 新しいセッションを作成し、サーバーを起動
echo "tmuxセッションを作成中..."
tmux new-session -d -s $SESSION "echo '=== Jabber Multi-Client Server (Port: $PORT) ==='; echo 'Compiling Java files...'; javac -d bin src/*.java && echo 'Starting server...' && java -cp bin JabberServer $PORT; echo 'Server stopped. Press any key to close.'; read"

# 2秒待機してサーバーが起動するのを待つ
sleep 2

# 最初のクライアントペインを右側に作成
echo "クライアント 1 を起動中..."
tmux split-window -h -t $SESSION "echo '=== Client 1 ==='; echo 'Connecting to server...'; java -cp bin JabberClient $PORT; echo 'Client 1 finished. Press any key to close.'; read"

# 残りのクライアントペインを下に分割して作成
for i in $(seq 2 $CLIENT_COUNT); do
    echo "クライアント $i を起動中..."
    tmux split-window -v -t $SESSION "echo '=== Client $i ==='; echo 'Connecting to server...'; java -cp bin JabberClient $PORT; echo 'Client $i finished. Press any key to close.'; read"
    sleep 0.5
done

# レイアウトを調整
case $CLIENT_COUNT in
    1)
        tmux select-layout -t $SESSION even-horizontal
        ;;
    2)
        tmux select-layout -t $SESSION even-horizontal
        ;;
    3)
        tmux select-layout -t $SESSION main-vertical
        ;;
    4)
        tmux select-layout -t $SESSION main-horizontal
        ;;
    *)
        tmux select-layout -t $SESSION tiled
        ;;
esac

# 最初のペイン（サーバー）を選択
tmux select-pane -t $SESSION:0

echo ""
echo "=== Jabber Multi-Client Test Session ==="
echo "セッション名: $SESSION"
echo "サーバーポート: $PORT"
echo "クライアント数: $CLIENT_COUNT"
echo ""
echo "ペイン構成:"
echo "- 左側/上部: マルチクライアントサーバー"
echo "- 右側/下部: クライアント 1-$CLIENT_COUNT"
echo ""
echo "操作方法:"
echo "- Ctrl+B → 矢印キー: ペイン間移動"
echo "- Ctrl+B → q: ペイン番号表示"
echo "- Ctrl+B → z: アクティブペインをズーム/縮小"
echo "- Ctrl+B → x: 現在のペインを閉じる"
echo "- Ctrl+B → d: セッションをデタッチ（バックグラウンド実行）"
echo "- 完全終了: tmux kill-session -t $SESSION"
echo ""
echo "サーバーログで各クライアントの接続状況を確認できます。"
echo "アタッチします..."

# tmuxセッションにアタッチ
tmux attach-session -t $SESSION 