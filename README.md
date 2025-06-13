# Jabber - Java Socket Communication System

## 概要
JabberはJavaベースのクライアント・サーバー型ソケット通信システムです。TCPプロトコルを使用してクライアントとサーバー間でメッセージのやり取りを行います。**複数のクライアントが同時に接続可能**なマルチスレッド対応サーバーです。

## 外部設計

### システム構成
```
[JabberClient] ←─TCP/IP─→ [JabberServer] ←─TCP/IP─→ [JabberClient]
     ポート指定          指定ポートでリッスン          ポート指定
                              ↑
                       [JabberClient]
                          ポート指定
```

### 主要コンポーネント

#### JabberServer（マルチクライアント対応）
- **役割**: TCPサーバーとして動作し、複数のクライアントからの接続を同時に処理
- **機能**: 
  - 指定ポートで複数のクライアント接続を受け付け
  - 各クライアントを個別のスレッドで処理
  - 受信したメッセージをクライアントIDと共にエコーバック
  - "END"コマンドで個別クライアント接続終了
- **特徴**:
  - マルチスレッド処理によりクライアント数に制限なし
  - 各クライアントにユニークなIDを自動割り当て
  - クライアントの接続・切断状況をリアルタイム表示
- **インターフェース**:
  - 起動: `java -cp bin JabberServer [ポート番号]`
  - デフォルトポート: 8080

#### JabberClient  
- **役割**: TCPクライアントとしてサーバーに接続
- **機能**:
  - サーバーへの接続確立
  - テストメッセージの送信（"howdy 0"〜"howdy 9"）
  - サーバーからの応答受信（クライアントIDを含む）
- **インターフェース**:
  - 起動: `java -cp bin JabberClient [ポート番号]`

### 通信プロトコル
1. **接続フェーズ**: クライアントがサーバーの指定ポートに接続
2. **データ交換フェーズ**: 
   - クライアント→サーバー: テキストメッセージ送信
   - サーバー→クライアント: 受信メッセージをクライアントIDと共にエコーバック
3. **終了フェーズ**: クライアントが"END"コマンドを送信して接続終了

### 開発環境
- **言語**: Java
- **ビルドツール**: javac
- **実行環境**: tmuxを使用したマルチペイン開発環境
- **起動スクリプト**: 
  - 単一クライアント: `./run.sh`
  - 複数クライアント: `./run_multi_client_test.sh`

## 使用方法

### 1. 単一クライアントテスト（従来通り）
```bash
# デフォルトポート(9000)で起動
./run.sh

# カスタムポートで起動
./run.sh 8080
```

### 2. 複数クライアントテスト（新機能）
```bash
# デフォルト設定（ポート9000、クライアント3個）
./run_multi_client_test.sh

# カスタム設定（ポート8080、クライアント5個）
./run_multi_client_test.sh 8080 5
```

#### 複数クライアントテスト操作方法
- **Ctrl+B → 矢印キー**: ペイン間移動
- **Ctrl+B → q**: ペイン番号表示
- **Ctrl+B → z**: アクティブペインをズーム/縮小
- **Ctrl+B → x**: 現在のペインを閉じる
- **Ctrl+B → d**: セッションをデタッチ（バックグラウンド実行）
- **完全終了**: `tmux kill-session -t jabber_multi_test`

### 3. 手動起動
```bash
# コンパイル
javac -d bin src/*.java

# サーバー起動（別ターミナル）
java -cp bin JabberServer 9000

# 複数のクライアントを起動（それぞれ別ターミナル）
java -cp bin JabberClient 9000
java -cp bin JabberClient 9000
java -cp bin JabberClient 9000
```

## マルチクライアント機能の詳細

### アーキテクチャ
- **スレッドプール**: `ExecutorService`を使用した効率的なスレッド管理
- **クライアント管理**: `AtomicInteger`でクライアントIDを安全に管理
- **例外処理**: 個別クライアントのエラーが他のクライアントに影響しない設計

### 同時接続可能数
- 理論上制限なし（システムリソースに依存）
- 各クライアント接続は独立したスレッドで処理
- クライアントの切断は他のクライアントに影響なし

### ログ出力例
```
=== Jabber Multi-Client Server (Port: 9000) ===
Compiling Java files...
Starting server...
Multi-client server started: ServerSocket[addr=0.0.0.0/0.0.0.0,localport=9000]
Waiting for client connections on port 9000...
New client connected (ID: 1): Socket[addr=/127.0.0.1,port=64301,localport=9000]
Client 1 handler started
New client connected (ID: 2): Socket[addr=/127.0.0.1,port=64302,localport=9000]
Client 2 handler started
Client 1 says: howdy 0
Client 2 says: howdy 0
Client 1 disconnected
Client 2 disconnected
```

### 画面表示例
```
┌─────────────────────────────────┬─────────────────────────────────┐
│=== Jabber Multi-Client Server  │=== Client 1 ===                │
│    (Port: 9000) ===             │Connecting to server...          │
│Compiling Java files...          │addr = localhost/127.0.0.1       │
│Starting server...               │socket = Socket[addr=localhost/  │
│Multi-client server started...   │127.0.0.1,port=9000,localport=  │
│Waiting for client connections...│64301]                           │
│New client connected (ID: 1)...  │howdy 0                          │
│Client 1 handler started         │Echo from server (Client 1)...  │
│New client connected (ID: 2)...  ├─────────────────────────────────┤
│Client 2 handler started         │=== Client 2 ===                │
│Client 1 says: howdy 0           │Connecting to server...          │
│Client 2 says: howdy 0           │addr = localhost/127.0.0.1       │
│                                 │socket = Socket[addr=localhost/  │
│                                 │127.0.0.1,port=9000,localport=  │
│                                 │64302]                           │
│                                 │howdy 0                          │
│                                 │Echo from server (Client 2)...  │
└─────────────────────────────────┴─────────────────────────────────┘
```