package com.example.marketplace.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.marketplace.model.CategoriaProduto;
import com.example.marketplace.model.ItemCarrinho;
import com.example.marketplace.model.Produto;
import com.example.marketplace.model.ResumoCarrinho;
import com.example.marketplace.model.SelecaoCarrinho;
import com.example.marketplace.repository.ProdutoRepository;

@Service
public class ServicoCarrinho {

    private final ProdutoRepository repositorioProdutos;

    public ServicoCarrinho(ProdutoRepository repositorioProdutos) {
        this.repositorioProdutos = repositorioProdutos;
    }

    public ResumoCarrinho construirResumo(List<SelecaoCarrinho> selecoes) {

        List<ItemCarrinho> itens = new ArrayList<>();

        // =========================
        // Monta os itens do carrinho
        // =========================
        for (SelecaoCarrinho selecao : selecoes) {
            Produto produto = repositorioProdutos.buscarPorId(selecao.getProdutoId())
                    .orElseThrow(
                            () -> new IllegalArgumentException("Produto não encontrado: " + selecao.getProdutoId()));

            itens.add(new ItemCarrinho(produto, selecao.getQuantidade()));
        }

        // =========================
        // Calcula subtotal
        // =========================
        BigDecimal subtotal = itens.stream()
                .map(ItemCarrinho::calcularSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // =========================
        // Calcula desconto por quantidade
        // =========================
        int quantidadeTotal = itens.stream()
                .mapToInt(ItemCarrinho::getQuantidade)
                .sum();
        BigDecimal descontoQuantidade = calcularDescontoQuantidade(quantidadeTotal);

        // =========================
        // Calcula desconto por categoria
        // =========================
        BigDecimal descontoCategoria = BigDecimal.ZERO;
        for (ItemCarrinho item : itens) {
            BigDecimal descontoPorItem = calcularDescontoCategoria(item.getProduto().getCategoria())
                    .multiply(BigDecimal.valueOf(item.getQuantidade()));
            descontoCategoria = descontoCategoria.add(descontoPorItem);
        }

        // =========================
        // Percentual total de desconto
        // =========================
        BigDecimal percentualDesconto = descontoQuantidade.add(descontoCategoria);
        if (percentualDesconto.compareTo(new BigDecimal("25")) > 0) {
            percentualDesconto = new BigDecimal("25");
        }

        // =========================
        // Valor do desconto e total
        // =========================
        BigDecimal valorDesconto = subtotal.multiply(percentualDesconto).divide(new BigDecimal("100"));
        BigDecimal total = subtotal.subtract(valorDesconto);

        return new ResumoCarrinho(itens, subtotal, percentualDesconto.divide(new BigDecimal("100")), valorDesconto, total);
    }

    private BigDecimal calcularDescontoQuantidade(int quantidade) {
        switch (quantidade) {
            case 1:
                return BigDecimal.ZERO;
            case 2:
                return new BigDecimal("5");
            case 3:
                return new BigDecimal("7");
            default:
                return new BigDecimal("10");
        }
    }

    private BigDecimal calcularDescontoCategoria(CategoriaProduto categoria) {
        switch (categoria) {
            case CAPINHA:
                return new BigDecimal("3");
            case CARREGADOR:
                return new BigDecimal("5");
            case FONE:
                return new BigDecimal("3");
            case PELICULA:
                return new BigDecimal("2");
            case SUPORTE:
                return new BigDecimal("2");
            default:
                return BigDecimal.ZERO;
        }
    }
}
