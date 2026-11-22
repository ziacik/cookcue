package com.ziacik.cookcue.mobile

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.print.pdf.PrintedPdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.ziacik.cookcue.core.model.CookingTask
import com.ziacik.cookcue.core.model.Recipe
import com.ziacik.cookcue.core.model.TaskKind
import com.ziacik.cookcue.core.scheduler.Scheduler
import java.io.FileOutputStream

/**
 * Prints the complete selected recipe as a compact, two-column document.
 * The print system also offers "Save as PDF" without requiring file permissions.
 */
internal object RecipePrinter {
	fun print(context: Context, recipe: Recipe) {
		val manager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
		manager.print(
			"CookCue - " + recipe.title,
			RecipePrintAdapter(context, recipe),
			PrintAttributes.Builder()
				.setMediaSize(PrintAttributes.MediaSize.ISO_A4)
				.setMinMargins(PrintAttributes.Margins(250, 250, 250, 250))
				.setResolution(PrintAttributes.Resolution("cookcue", "CookCue", 300, 300))
				.setColorMode(PrintAttributes.COLOR_MODE_MONOCHROME)
				.build(),
		)
	}
}

private class RecipePrintAdapter(
	private val context: Context,
	private val recipe: Recipe,
) : PrintDocumentAdapter() {
	private var attributes: PrintAttributes? = null

	override fun onLayout(
		oldAttributes: PrintAttributes?,
		newAttributes: PrintAttributes,
		cancellationSignal: CancellationSignal,
		callback: LayoutResultCallback,
		extras: Bundle?,
	) {
		if (cancellationSignal.isCanceled) {
			callback.onLayoutCancelled()
			return
		}
		attributes = newAttributes
		callback.onLayoutFinished(
			PrintDocumentInfo.Builder(recipe.title + ".pdf")
				.setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
				.setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
				.build(),
			oldAttributes != newAttributes,
		)
	}

	override fun onWrite(
		pages: Array<out PageRange>,
		destination: ParcelFileDescriptor,
		cancellationSignal: CancellationSignal,
		callback: WriteResultCallback,
	) {
		val printAttributes = attributes
		if (printAttributes == null) {
			callback.onWriteFailed("Chýba nastavenie tlače.")
			return
		}
		if (cancellationSignal.isCanceled) {
			callback.onWriteCancelled()
			return
		}

		try {
			val document = PrintedPdfDocument(context, printAttributes)
			try {
				val bounds = document.pageContentRect
				val composer = CompactRecipeComposer(
					recipe = recipe,
					width = bounds.width().toFloat(),
					height = bounds.height().toFloat(),
				)
				// Preserve comfortable type whenever possible. Reduce it only if
				// that allows the whole recipe to fit on one sheet.
				val plan = listOf(1f, 0.96f, 0.92f, 0.88f)
					.asSequence()
					.map(composer::compose)
					.firstOrNull { it.pages.size == 1 }
					?: composer.compose(1f)
				val written = mutableListOf<PageRange>()

				plan.pages.forEachIndexed { index, placements ->
					if (cancellationSignal.isCanceled) return@forEachIndexed
					if (pages.none { index in it.start..it.end }) return@forEachIndexed

					val page = document.startPage(index + 1)
					val canvas = page.canvas
					canvas.save()
					canvas.translate(bounds.left.toFloat(), bounds.top.toFloat())
					placements.forEach { it.draw(canvas) }

					if (index == 0) {
						val rule = Paint(Paint.ANTI_ALIAS_FLAG).apply {
							color = Color.LTGRAY
							strokeWidth = 0.6f
						}
						canvas.drawLine(0f, plan.dividerY, bounds.width().toFloat(), plan.dividerY, rule)
					}

					val footer = Paint(Paint.ANTI_ALIAS_FLAG).apply {
						color = Color.DKGRAY
						textSize = 7f
					}
					canvas.drawText(
						"CookCue · " + (index + 1) + "/" + plan.pages.size,
						0f,
						bounds.height() - 1f,
						footer,
					)
					canvas.restore()
					document.finishPage(page)
					written += PageRange(index, index)
				}

				if (cancellationSignal.isCanceled) {
					callback.onWriteCancelled()
					return
				}
				FileOutputStream(destination.fileDescriptor).use(document::writeTo)
				callback.onWriteFinished(written.toTypedArray())
			} finally {
				document.close()
			}
		} catch (error: Exception) {
			callback.onWriteFailed(error.localizedMessage ?: "Nepodarilo sa pripraviť tlač.")
		} finally {
			destination.close()
		}
	}
}

private enum class PrintStyle {
	TITLE,
	SUBTITLE,
	SECTION,
	STEP,
	BODY,
	SMALL,
}

private data class PrintLine(val text: String, val style: PrintStyle)

private data class PrintBlock(
	val lines: List<PrintLine>,
	val spaceBefore: Float = 0f,
	val keepWithNext: Boolean = false,
)

private data class PlacedText(
	val layout: StaticLayout,
	val x: Float,
	val y: Float,
	val startLine: Int,
	val endLine: Int,
) {
	fun draw(canvas: Canvas) {
		canvas.save()
		val top = layout.getLineTop(startLine)
		val bottom = layout.getLineBottom(endLine - 1)
		canvas.clipRect(x, y, x + layout.width, y + bottom - top)
		canvas.translate(x, y - top)
		layout.draw(canvas)
		canvas.restore()
	}
}

private data class PrintPlan(
	val pages: List<List<PlacedText>>,
	val dividerY: Float,
)

private class CompactRecipeComposer(
	private val recipe: Recipe,
	private val width: Float,
	private val height: Float,
) {
	private val columnCount = when {
		width >= 710f -> 3
		width >= 420f -> 2
		else -> 1
	}
	private val gutter = 15f
	private val columnWidth = (width - gutter * (columnCount - 1)) / columnCount

	fun compose(scale: Float): PrintPlan {
		val pages = mutableListOf<MutableList<PlacedText>>(mutableListOf())
		var headerY = 0f

		fun textLayout(text: String, style: PrintStyle, lineWidth: Float): StaticLayout {
			val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
				textSize = when (style) {
					PrintStyle.TITLE -> 16f
					PrintStyle.SUBTITLE -> 8.4f
					PrintStyle.SECTION -> 9.5f
					PrintStyle.STEP -> 8.8f
					PrintStyle.BODY -> 8.2f
					PrintStyle.SMALL -> 7.5f
				} * scale
				color = if (style == PrintStyle.SMALL || style == PrintStyle.SUBTITLE) {
					Color.DKGRAY
				} else {
					Color.BLACK
				}
				typeface = if (style == PrintStyle.TITLE || style == PrintStyle.SECTION ||
					style == PrintStyle.STEP
				) {
					Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
				} else {
					Typeface.DEFAULT
				}
			}
			return StaticLayout.Builder.obtain(
				text,
				0,
				text.length,
				paint,
				lineWidth.toInt().coerceAtLeast(1),
			)
				.setAlignment(Layout.Alignment.ALIGN_NORMAL)
				.setIncludePad(false)
				.build()
		}

		val header = buildList {
			add(PrintLine(recipe.title, PrintStyle.TITLE))
			add(PrintLine("Porcie: " + recipe.servings, PrintStyle.SUBTITLE))
			if (recipe.description.isNotBlank()) {
				add(PrintLine(recipe.description, PrintStyle.SUBTITLE))
			}
		}
		header.forEach { line ->
			val layout = textLayout(line.text, line.style, width)
			pages[0] += PlacedText(layout, 0f, headerY, 0, layout.lineCount)
			headerY += layout.height + 2f * scale
		}

		val dividerY = headerY + 4f
		val firstColumnTop = dividerY + 11f
		val columnBottom = height - 14f
		var pageIndex = 0
		var columnIndex = 0
		var cursorY = firstColumnTop

		fun columnTop() = if (pageIndex == 0) firstColumnTop else 0f

		fun advanceColumn() {
			if (columnIndex < columnCount - 1) {
				columnIndex++
			} else {
				columnIndex = 0
				pageIndex++
				pages += mutableListOf<PlacedText>()
			}
			cursorY = columnTop()
		}

		fun place(block: PrintBlock) {
			val prepared = block.lines.filter { it.text.isNotBlank() }
				.map { it to textLayout(it.text, it.style, columnWidth) }
			if (prepared.isEmpty()) return

			val fullHeight = block.spaceBefore * scale +
				prepared.sumOf { it.second.height.toDouble() + 1.5 * scale }.toFloat()
			val columnCapacity = columnBottom - columnTop()
			if (cursorY > columnTop() && fullHeight <= columnCapacity &&
				cursorY + fullHeight > columnBottom
			) {
				advanceColumn()
			}
			if (cursorY + block.spaceBefore * scale >= columnBottom) {
				advanceColumn()
			} else {
				cursorY += block.spaceBefore * scale
			}

			prepared.forEach { (_, layout) ->
				var firstLine = 0
				while (firstLine < layout.lineCount) {
					var endLine = firstLine
					while (endLine < layout.lineCount &&
						layout.getLineBottom(endLine) - layout.getLineTop(firstLine) <=
						columnBottom - cursorY
					) {
						endLine++
					}
					if (endLine == firstLine) {
						advanceColumn()
						continue
					}
					val visibleHeight =
						layout.getLineBottom(endLine - 1) - layout.getLineTop(firstLine)
					pages[pageIndex] += PlacedText(
						layout = layout,
						x = columnIndex * (columnWidth + gutter),
						y = cursorY,
						startLine = firstLine,
						endLine = endLine,
					)
					cursorY += visibleHeight
					firstLine = endLine
					if (firstLine < layout.lineCount) advanceColumn()
				}
				cursorY += 1.5f * scale
			}
		}

		val blocks = recipeBlocks(recipe)
		blocks.forEachIndexed { index, block ->
			if (block.keepWithNext) {
				val next = blocks.getOrNull(index + 1)
				if (next != null) {
					fun blockHeight(value: PrintBlock): Float =
						value.spaceBefore * scale + value.lines.sumOf {
							(textLayout(it.text, it.style, columnWidth).height + 1.5f * scale).toDouble()
						}.toFloat()
					val combinedHeight = blockHeight(block) + blockHeight(next)
					if (cursorY > columnTop() && combinedHeight <= columnBottom - columnTop() &&
						cursorY + combinedHeight > columnBottom
					) {
						advanceColumn()
					}
				}
			}
			place(block)
		}
		return PrintPlan(pages.map { it.toList() }, dividerY)
	}
}

private fun recipeBlocks(recipe: Recipe): List<PrintBlock> = buildList {
	if (recipe.ingredients.isNotEmpty()) {
		add(PrintBlock(listOf(PrintLine("INGREDIENCIE", PrintStyle.SECTION)), keepWithNext = true))
		recipe.ingredients.forEach { ingredient ->
			val text = listOf(ingredient.amount, ingredient.name)
				.filter(String::isNotBlank)
				.joinToString("  ")
			add(PrintBlock(listOf(PrintLine("• " + text, PrintStyle.BODY)), 2f))
		}
	}
	recipe.preCookingNote?.takeIf(String::isNotBlank)?.let { note ->
		add(PrintBlock(listOf(
			PrintLine("PRED VARENÍM", PrintStyle.SECTION),
			PrintLine(note, PrintStyle.BODY),
		), 9f))
	}
	add(PrintBlock(listOf(PrintLine("POSTUP", PrintStyle.SECTION)), 10f, keepWithNext = true))

	val plan = Scheduler().schedule(recipe)
	val groupedTasks = plan.groupBy { it.task.repeatInfo?.id ?: it.task.id }
	groupedTasks.values.forEachIndexed { index, group ->
		val first = group.first()
		val repeat = first.task.repeatInfo
		if (repeat == null) {
			add(PrintBlock(stepLines(
				index + 1,
				first.task,
				first.startSeconds,
			), 5f))
		} else {
			val lines = mutableListOf(
				PrintLine(
					(index + 1).toString() + ". " + repeat.title + " (" + repeat.count +
						"× opakovať)",
					PrintStyle.STEP,
				),
			)
			group.filter { it.task.repeatInfo?.repetition == 1 }.forEach { phase ->
				val task = phase.task
				val name = task.title
					.removePrefix(repeat.id.uppercase() + " · ")
					.removeSuffix(" · 1/" + repeat.count)
				lines += PrintLine(
					"• " + name + " · " + shortDuration(task.durationSeconds),
					PrintStyle.BODY,
				)
				if (task.instruction != task.title) {
					lines += PrintLine(task.instruction, PrintStyle.SMALL)
				}
				task.tips.forEach { lines += PrintLine("Tip: " + it, PrintStyle.SMALL) }
			}
			add(PrintBlock(lines, 5f))
		}
	}

	if (recipe.troubleshooting.isNotEmpty()) {
		add(PrintBlock(listOf(PrintLine("KEĎ SA NEDARÍ", PrintStyle.SECTION)), 11f, keepWithNext = true))
		recipe.troubleshooting.forEach { tip ->
			add(PrintBlock(listOf(
				PrintLine(tip.problem, PrintStyle.STEP),
				PrintLine(tip.advice, PrintStyle.SMALL),
			), 4f))
		}
	}
}

private fun stepLines(number: Int, task: CookingTask, startsAt: Long): List<PrintLine> =
	buildList {
		val optional = if (task.optional) " · voliteľné" else ""
		add(PrintLine(
			number.toString() + ". " + task.title + " · " +
				shortDuration(task.durationSeconds) + " (T+" +
				shortDuration(startsAt) + ")" + optional,
			PrintStyle.STEP,
		))
		if (task.instruction.isNotBlank() && task.instruction != task.title) {
			add(PrintLine(task.instruction, PrintStyle.BODY))
		}
		if (task.kind == TaskKind.EVENT && !task.actionLabel.isNullOrBlank()) {
			add(PrintLine("Potvrď: " + task.actionLabel, PrintStyle.SMALL))
		}
		task.tips.forEach { add(PrintLine("Tip: " + it, PrintStyle.SMALL)) }
	}

private fun shortDuration(seconds: Long): String {
	if (seconds == 0L) return "0m"
	if (seconds < 60) return seconds.toString() + "s"
	val minutes = seconds / 60
	val remainingSeconds = seconds % 60
	return if (remainingSeconds == 0L) {
		minutes.toString() + "m"
	} else {
		minutes.toString() + "m " + remainingSeconds + "s"
	}
}
