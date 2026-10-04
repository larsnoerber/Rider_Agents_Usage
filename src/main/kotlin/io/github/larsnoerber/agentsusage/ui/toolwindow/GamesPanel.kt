package io.github.larsnoerber.agentsusage.ui.toolwindow

import com.intellij.ide.util.PropertiesComponent
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import io.github.larsnoerber.agentsusage.settings.AgentsUsageSettings
import io.github.larsnoerber.agentsusage.ui.components.UsageSurface
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Font
import java.awt.GridLayout
import javax.swing.JButton
import javax.swing.JPanel

/** Small local games area for the overview. */
internal class GamesPanel : JPanel(BorderLayout(0, JBUI.scale(5))) {
    private val properties = PropertiesComponent.getInstance()
    private val settings = AgentsUsageSettings.getInstance()
    private val toggle = JButton()
    private val surface = UsageSurface()
    private val body = JPanel().apply {
        layout = javax.swing.BoxLayout(this, javax.swing.BoxLayout.Y_AXIS)
        isOpaque = false
        maximumSize = Dimension(Int.MAX_VALUE, Int.MAX_VALUE)
    }
    private val status = JBLabel()
    private val score = JBLabel()
    private val cells = Array(9) { JButton() }
    private val board = arrayOfNulls<Char>(9)
    private var finished = false

    init {
        isOpaque = false
        border = JBUI.Borders.emptyTop(8)
        surface.addRow(toggle.apply {
            text = "GAMES"
            horizontalAlignment = JButton.LEFT
            isContentAreaFilled = false
            isBorderPainted = false
            isFocusPainted = false
            margin = JBUI.emptyInsets()
            font = font.deriveFont(Font.BOLD, 11f)
            addActionListener { setExpanded(!settings.state.gamesExpanded) }
        })
        addBodyRow(JBLabel("Tic-Tac-Toe").apply {
            font = font.deriveFont(Font.BOLD, 13f)
        }, 6)
        surface.addRow(JBLabel("You are O · AI is X · You start").apply {
            foreground = secondaryColor()
        }, JBUI.scale(2))
        addBodyRow(score, 4)

        val grid = JPanel(GridLayout(3, 3, JBUI.scale(4), JBUI.scale(4))).apply {
            isOpaque = false
            preferredSize = JBUI.size(144, 144)
            maximumSize = preferredSize
        }
        cells.forEachIndexed { index, cell ->
            cell.apply {
                font = font.deriveFont(Font.BOLD, 22f)
                preferredSize = JBUI.size(44, 44)
                addActionListener { play(index) }
            }
            grid.add(cell)
        }
        val actions = JPanel(BorderLayout(JBUI.scale(8), 0)).apply {
            isOpaque = false
            add(status, BorderLayout.CENTER)
            add(JButton("New game").apply { addActionListener { startGame() } }, BorderLayout.EAST)
        }
        addBodyRow(JPanel(FlowLayout(FlowLayout.CENTER, 0, 0)).apply {
            isOpaque = false
            add(grid)
        }, 8)
        addBodyRow(actions, 6)
        surface.addRow(body)
        add(surface, BorderLayout.NORTH)
        setExpanded(settings.state.gamesExpanded)
        startGame()
    }

    private fun addBodyRow(component: java.awt.Component, bottomGap: Int = 5) {
        if (component is javax.swing.JComponent) component.alignmentX = LEFT_ALIGNMENT
        body.add(component)
        if (bottomGap > 0) body.add(javax.swing.Box.createVerticalStrut(JBUI.scale(bottomGap)))
    }

    private fun setExpanded(value: Boolean) {
        settings.state.gamesExpanded = value
        body.isVisible = value
        surface.components.drop(1).forEach { it.isVisible = value }
        toggle.text = if (value) "GAMES  ▾" else "GAMES  ▸"
        toggle.accessibleContext.accessibleName = if (value) "Collapse games" else "Expand games"
        revalidate()
    }

    private fun play(index: Int) {
        if (finished || board[index] != null) return
        board[index] = 'O'
        renderBoard()
        if (showResult()) {
            if (winner() == 'O') recordWin('O') else recordDraw()
            return
        }

        status.text = "AI (X) is thinking…"
        val move = bestMove()
        if (move >= 0) board[move] = 'X'
        renderBoard()
        if (showResult()) {
            if (winner() == 'X') recordWin('X') else recordDraw()
        } else {
            status.text = "Your turn (O)"
        }
    }

    private fun startGame() {
        board.fill(null)
        finished = false
        renderBoard()
        status.text = "Your turn (O)"
        renderScore()
    }

    private fun renderBoard() {
        cells.forEachIndexed { index, cell ->
            cell.text = board[index]?.toString() ?: " "
            cell.isEnabled = !finished && board[index] == null
            cell.foreground = when (board[index]) {
                'X' -> JBColor(Color(157, 119, 220), Color(195, 165, 245))
                'O' -> JBColor(Color(57, 139, 174), Color(119, 196, 224))
                else -> JBColor.foreground()
            }
        }
    }

    private fun showResult(): Boolean {
        when {
            winner() == 'O' -> status.text = "You win!"
            winner() == 'X' -> status.text = "AI wins. Try again?"
            board.all { it != null } -> status.text = "Draw. Try again?"
            else -> return false
        }
        finished = true
        renderBoard()
        return true
    }

    private fun recordWin(winner: Char) {
        val key = if (winner == 'O') USER_WINS_KEY else AI_WINS_KEY
        properties.setValue(key, (properties.getInt(key, 0) + 1).toString())
        renderScore()
    }

    private fun recordDraw() {
        properties.setValue(DRAWS_KEY, (properties.getInt(DRAWS_KEY, 0) + 1).toString())
        renderScore()
    }

    private fun renderScore() {
        score.text = "Wins — You: ${properties.getInt(USER_WINS_KEY, 0)} · AI: ${properties.getInt(AI_WINS_KEY, 0)} · Draws: ${properties.getInt(DRAWS_KEY, 0)}"
    }

    private fun bestMove(): Int {
        var bestScore = Int.MIN_VALUE
        var move = -1
        board.indices.filter { board[it] == null }.forEach { index ->
            board[index] = 'X'
            val score = minimax(isAiTurn = false, depth = 0)
            board[index] = null
            if (score > bestScore) {
                bestScore = score
                move = index
            }
        }
        return move
    }

    private fun minimax(isAiTurn: Boolean, depth: Int): Int {
        when (winner()) {
            'X' -> return 10 - depth
            'O' -> return depth - 10
        }
        if (board.all { it != null }) return 0

        val scores = board.indices.filter { board[it] == null }.map { index ->
            board[index] = if (isAiTurn) 'X' else 'O'
            val score = minimax(!isAiTurn, depth + 1)
            board[index] = null
            score
        }
        return if (isAiTurn) scores.max() else scores.min()
    }

    private fun winner(): Char? = WIN_LINES.firstNotNullOfOrNull { line ->
        val mark = board[line[0]]
        mark?.takeIf { line.all { index -> board[index] == mark } }
    }

    private fun secondaryColor() = JBColor(Color(105, 109, 117), Color(160, 163, 170))

    private companion object {
        const val USER_WINS_KEY = "agents.usage.games.tictactoe.userWins"
        const val AI_WINS_KEY = "agents.usage.games.tictactoe.aiWins"
        const val DRAWS_KEY = "agents.usage.games.tictactoe.draws"

        val WIN_LINES = listOf(
            intArrayOf(0, 1, 2), intArrayOf(3, 4, 5), intArrayOf(6, 7, 8),
            intArrayOf(0, 3, 6), intArrayOf(1, 4, 7), intArrayOf(2, 5, 8),
            intArrayOf(0, 4, 8), intArrayOf(2, 4, 6)
        )
    }
}
